(ns tech.opencv-compute-test
  (:require [tech.opencv :as opencv]
            [tech.resource :as resource]
            [clojure.test :refer :all]
            [tech.opencv-test :as opencv-test]
            [tech.v2.datatype :as dtype]
            [tech.v2.datatype.functional :as dfn]
            [tech.v2.tensor :as dtt]))


(deftest bgr-test
  []
  (resource/stack-resource-context
    (let [test-image (opencv/load "test/data/test.jpg")
          ;;Select is in-place so this did not change the image at all.
          bgr-image (dtt/select test-image :all :all [2 1 0])
          dest-image (dtype/copy! bgr-image (dtype/from-prototype test-image))]
      ;;The datatype library has the convention that the thing that is mutated
      ;;is returned from the function.
      (opencv/save dest-image "bgr.jpg"))))


(deftest lighten-bgr-test
  []
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


(deftest smooth-image-flow
  (resource/stack-resource-context
   (let [src-img (opencv/load "test/data/test.jpg")]
     (-> src-img
         (dfn// 2)
         (dtype/copy! (dtype/from-prototype src-img))
         (opencv/save "tensor_darken.jpg")))))
