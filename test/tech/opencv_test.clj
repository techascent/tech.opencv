(ns tech.opencv-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [tech.opencv :as opencv]
            [tech.resource :as resource]
            [tech.v2.datatype :as dtype]
            [tech.v2.datatype.jna :as dtype-jna]
            [tech.v2.datatype.functional :as dfn]
            [tech.v2.datatype.unary-op :as unary-op]
            [tech.jna :as jna]))

(defn delete-test-file!
  [test-fname]
  (when (.exists (io/file test-fname))
    (io/delete-file test-fname)))

(deftest base-test
  (resource/stack-resource-context
    (let [src-img (opencv/load "test/data/test.jpg")
          [height width n-chan] (dtype/shape src-img)
          test-fname "smaller.jpg"
          larger-fname "larger.jpg"]
      (delete-test-file! test-fname)
      (delete-test-file! larger-fname)
      (-> (opencv/resize src-img 128)
          (opencv/save test-fname))
      (is (.exists (io/file test-fname)))
      (-> (opencv/resize src-img 1024)
          (opencv/save larger-fname))
      (is (.exists (io/file larger-fname)))
      ;;Now go visually inspect results
      )))


(deftest marshal-test
  (resource/stack-resource-context
    (let [src-img (opencv/load "test/data/test.jpg")
          dest-img (opencv/clone src-img)
          num-elems (dtype/ecount src-img)
          ;;Save as jpg and spend an hour scratching head...
          test-fname "darken.png"
          convert-fn (fn [input]
                       (float (Math/floor (* 0.5 input))))]
      ;;darken img.  Float data is range 0-255
      (delete-test-file! test-fname)
      (-> (unary-op/unary-reader :int16 (unchecked-short (quot x 2)) src-img)
          (dtype/copy! dest-img)
          (opencv/save test-fname))
      (let [result (opencv/load test-fname)
            result-data (float-array num-elems)]
        (dtype/copy! result result-data)
        (is (dfn/equals (take 10 result-data)
                        (map convert-fn (take 10 (dtype/->reader src-img)))))))))


(deftest copy-raw
  (resource/stack-resource-context
    (let [src-image (opencv/load "test/data/test.jpg")
          test-buf (int-array (* 3 (dtype/ecount src-image)))]
      ;;Does this work or not.  Important functionality
      (dtype/copy-raw->item! (repeat 3 src-image) test-buf 0)
      (is (= [172 170 170 172 170 170 171 169 169 171]
             (vec (take 10 test-buf)))))))


(deftest correct-interfaces
  (resource/stack-resource-context
    (let [src-image (opencv/load "test/data/test.jpg")]
      (is (jna/checknil src-image))
      (is (jna/ensure-ptr src-image)))))
