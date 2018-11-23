(ns tech.opencv-compute-test
  (:require [tech.opencv :as opencv]
            [tech.compute.tensor :as ct]
            [tech.resource :as resource]
            [clojure.test :refer :all]
            [tech.compute.cpu.tensor-math :as cpu-tm]
            [tech.opencv-test :as opencv-test]
            [tech.compute.tensor.operations :as op]
            [tech.datatype.jna :as dtype-jna]))


(deftest bgr-test
  []
  (resource/stack-resource-context
    (let [test-image (opencv/load "test/data/test.jpg")
          ;;Select is in-place so this did not change the image at all.
          bgr-image (ct/select test-image :all :all [2 1 0])
          dest-tens (ct/clone bgr-image)]
      ;;The tensor library has the convention that the thing that is mutated
      ;;is the first thing.  Also the thing that is mutated is returned from
      ;;the function.
      (ct/assign! test-image dest-tens)
      (opencv/save test-image "bgr.jpg"))))


(deftest lighten-bgr-test
  []
  (resource/stack-resource-context
    (let [test-image (opencv/load "test/data/test.jpg")]
      (ct/assign! test-image (-> test-image
                                 (ct/select :all :all [2 1 0])
                                 (ct/clone :datatype :uint16)
                                 (op/+ 50)
                                 ;;Clamp top end to 0-255
                                 (op/min 255)))

      (opencv/save test-image "bgr-lighten.jpg"))))


(deftest smooth-image-flow
  (resource/stack-resource-context
    (-> (opencv/load "test/data/test.jpg")
        (op// 2)
        (opencv/save "tensor_darken.jpg"))))
