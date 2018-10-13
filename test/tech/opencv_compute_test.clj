(ns tech.opencv-compute-test
  (:require [tech.opencv :as opencv]
            [tech.compute.tensor :as ct]
            [think.resource.core :as resource]
            [clojure.test :refer :all]
            [tech.compute.cpu.tensor-math :as cpu-tm]
            [tech.opencv-test :as opencv-test]
            [tech.compute.tensor.operations :as op]))


(ct/enable-cpu-tensors!)



(deftest bgr-test
  []
  (resource/with-resource-context
    (let [test-image (opencv/load "test/data/test.jpg")
          image-tens (cpu-tm/typed-bufferable->tensor test-image)
          ;;Select is in-place so this did not change the image at all.
          bgr-image (ct/select image-tens :all :all [2 1 0])
          dest-tens (-> (ct/new-tensor (ct/shape bgr-image)
                                       :datatype (ct/get-datatype image-tens))
                        (ct/assign! bgr-image))]
      ;;The tensor library has the convention that the thing that is mutated
      ;;is the first thing.  Also the thing that is mutated is returned from
      ;;the function.
      (ct/assign! image-tens dest-tens)
      (opencv/save test-image "bgr.jpg"))))


(deftest lighten-bgr-test
  []
  (resource/with-resource-context
    (let [test-image (opencv/load "test/data/test.jpg")
          image-tens (cpu-tm/typed-bufferable->tensor test-image)
          bgr-img (ct/select image-tens :all :all [2 1 0])]
      (ct/assign! image-tens (-> (ct/new-tensor (ct/shape bgr-img) :datatype :uint16)
                                 (ct/assign! bgr-img)
                                 (op/+ 50)
                                 ;;Clamp top end to 0-255
                                 (op/min 255)))

      (opencv/save test-image "bgr-lighten.jpg"))))