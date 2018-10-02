(ns tech.opencv-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [tech.opencv :as opencv]
            [think.resource.core :as resource]
            [clojure.core.matrix :as m]))

(defn- delete-test-file!
  [test-fname]
  (when (.exists (io/file test-fname))
    (io/delete-file test-fname)))

(deftest base-test
  (resource/with-resource-context
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
