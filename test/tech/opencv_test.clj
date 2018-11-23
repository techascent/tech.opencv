(ns tech.opencv-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [tech.opencv :as opencv]
            [tech.resource :as resource]
            [clojure.core.matrix :as m]
            [clojure.core.matrix.macros :refer [c-for]]
            [tech.datatype :as dtype]
            [tech.datatype.base :as dtype-base]
            [tech.datatype.java-unsigned :as unsigned]
            [tech.datatype.jna :as dtype-jna]
            [tech.jna :as jna]))

(defn delete-test-file!
  [test-fname]
  (when (.exists (io/file test-fname))
    (io/delete-file test-fname)))

(deftest base-test
  (resource/stack-resource-context
    (let [src-img (opencv/load "test/data/test.jpg")
          [height width n-chan] (m/shape src-img)
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
    (with-bindings {#'dtype-base/*error-on-slow-path* true}
      (let [src-img (opencv/load "test/data/test.jpg")
            dest-img (opencv/clone src-img)
            num-elems (m/ecount src-img)
            float-data (float-array num-elems)
            mult-data (float-array num-elems)
            ;;Save as jpg and spend an hour scratching head...
            test-fname "darken.png"
            convert-fn (fn [input]
                         (float (Math/round (* 0.5 input))))]
        (dtype/copy! src-img float-data)
        ;;darken img.  Float data is range 0-255
        (c-for [idx (int 0) (< idx num-elems) (inc idx)]
               (aset mult-data idx (float
                                    (convert-fn (aget float-data idx)))))
        (delete-test-file! test-fname)
        (-> (dtype/copy! mult-data dest-img)
            (opencv/save test-fname))
        (let [result (opencv/load test-fname)
              result-data (float-array num-elems)]
          (dtype/copy! result result-data)
          (is (m/equals (take 10 result-data)
                        (map convert-fn (take 10 float-data)))))))))


(deftest copy-raw
  (resource/stack-resource-context
    (let [src-image (opencv/load "test/data/test.jpg")
          test-buf (int-array (* 3 (m/ecount src-image)))]
      ;;Does this work or not.  Important functionality
      (dtype/copy-raw->item! (repeat 3 src-image) test-buf 0)
      (is (= [172 170 170 172 170 170 171 169 169 171]
             (vec (take 10 test-buf)))))))


(deftest correct-interfaces
  (resource/stack-resource-context
    (let [src-image (opencv/load "test/data/test.jpg")]
      (is (dtype-jna/typed-pointer? src-image)
          (unsigned/typed-buffer? src-image))
      (is (jna/checknil src-image))
      (is (jna/ensure-ptr src-image)))))
