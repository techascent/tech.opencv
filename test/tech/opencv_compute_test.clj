(ns tech.opencv-compute-test
  (:require [tech.opencv :as opencv]
            [tech.resource :as resource]
            [clojure.test :refer :all]
            [tech.opencv-test :as opencv-test]
            [tech.v2.datatype :as dtype]
            [tech.v2.datatype.functional :as dfn]
            [tech.v2.datatype.unary-op :as unary-op]
            [tech.v2.tensor :as dtt]
            [clojure.pprint]))


(deftest bgr-test
  (resource/stack-resource-context
    (let [test-image (opencv/load "test/data/test.jpg")
          ;;Select is in-place so this did not change the image at all.
          bgr-image (dtt/select test-image :all :all [2 1 0])
          dest-image (dtype/copy! bgr-image (dtype/from-prototype test-image))]
      ;;The datatype library has the convention that the thing that is mutated
      ;;is returned from the function.
      (opencv/save dest-image "bgr.jpg"))))


(deftest lighten-bgr-test
  (resource/stack-resource-context
   (let [test-image (opencv/load "test/data/test.jpg")
         result
         (-> test-image
             (dtt/select :all :all [2 1 0])
             (dfn/+ 50)
             ;;Clamp top end to 0-255
             (dfn/min 255)
             (dtype/copy! (dtype/from-prototype test-image)))]

     (opencv/save result "bgr-lighten.jpg"))))


(deftest modify-time-test
  (resource/stack-resource-context
   ;;Clone test-image into jvm datastructures so the garbage collector
   ;;collects the garbage from the time tests below and we aren't sitting
   ;;with image allocation/deallocation times in our performance tests.
   ;;The gc will be a lot faster.
   (let [source-image (-> (dtt/clone (opencv/load "test/data/test.jpg")
                                   :container-type :typed-buffer))

         ;; Reader composition is lazy so the expression below reads from
         ;; the test image (ecount image) times.  It writes to the destination
         ;; once and the byte value is completely transformed from the src image
         ;; to the dest while in cache.  Virtual table lookups happen multiple
         ;; times per byte value.  ;; It is important to realize that under the
         ;; covers the image is stored as bytes.  These are read in a datatype-aware
         ;; way and converted to their appropriate unsigned values automatically
         ;; and when writter they are checked to ensure they are within range.
         ;; There are 2N checks for correct datatype in this pathway; everything else
         ;; is read/operated on as a short integer.
         reader-composition  #(-> source-image
                                  (dtt/select :all :all [2 1 0])
                                  (dfn/+ 50)
                                  ;;Clamp top end to 0-255
                                  (dfn/min 255)
                                  (dtype/copy! (dtype/from-prototype source-image)))

         inline-fn #(as-> source-image dest-image
                      (dtt/select dest-image :all :all [2 1 0])
                      (unary-op/unary-reader
                       :int16 (-> (+ x 50)
                                  (min 255)
                                  unchecked-short)
                       dest-image)
                      (dtype/copy! dest-image
                                   ;;Note from-prototype fails for reader chains.
                                   ;;So you have to copy or use an actual image.
                                   (dtype/from-prototype source-image)))]
     ;;warmup a little.
     (reader-composition)
     (inline-fn)
     (clojure.pprint/pprint
      {:reader-composition (with-out-str (time (dotimes [iter 50]
                                                 (reader-composition))))
       :inline-fn (with-out-str (time (dotimes [iter 50]
                                        (inline-fn))))}))))


(deftest smooth-image-flow
  (resource/stack-resource-context
   (let [src-img (opencv/load "test/data/test.jpg")]
     (-> src-img
         (dfn// 2)
         (dtype/copy! (dtype/from-prototype src-img))
         (opencv/save "tensor_darken.jpg")))))
