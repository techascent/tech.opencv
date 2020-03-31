(ns tech.opencv
  (:require [tech.resource.stack :as stack]
            [tech.resource :as resource]
            [tech.jna :as jna]
            [tech.v2.datatype :as dtype]
            [tech.v2.datatype.casting :as casting]
            [tech.v2.datatype.protocols :as dtype-proto]
            [tech.v2.datatype.javacpp :as jcpp-dtype]
            [tech.v2.tensor.dimensions.analytics :as dims-analytics]
            [clojure.set :as c-set])
  (:refer-clojure :exclude [load])
  (:import [org.bytedeco.javacpp opencv_core
            opencv_imgcodecs opencv_core$Mat
            opencv_imgproc opencv_core$Size]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)


(def opencv-type->channels-datatype-map
  {opencv_core/CV_8SC1 {:n-channels 1
                        :datatype :int8}
   opencv_core/CV_8SC2 {:n-channels 2
                        :datatype :int8}
   opencv_core/CV_8SC3 {:n-channels 3
                        :datatype :int8}
   opencv_core/CV_8SC4 {:n-channels 4
                        :datatype :int8}

   opencv_core/CV_8UC1 {:n-channels 1
                        :datatype :uint8}
   opencv_core/CV_8UC2 {:n-channels 2
                        :datatype :uint8}
   opencv_core/CV_8UC3 {:n-channels 3
                        :datatype :uint8}
   opencv_core/CV_8UC4 {:n-channels 4
                        :datatype :uint8}

   opencv_core/CV_16SC1 {:n-channels 1
                        :datatype :int16}
   opencv_core/CV_16SC2 {:n-channels 2
                        :datatype :int16}
   opencv_core/CV_16SC3 {:n-channels 3
                        :datatype :int16}
   opencv_core/CV_16SC4 {:n-channels 4
                        :datatype :int16}

   opencv_core/CV_16UC1 {:n-channels 1
                        :datatype :uint16}
   opencv_core/CV_16UC2 {:n-channels 2
                        :datatype :uint16}
   opencv_core/CV_16UC3 {:n-channels 3
                        :datatype :uint16}
   opencv_core/CV_16UC4 {:n-channels 4
                         :datatype :uint16}

   opencv_core/CV_32SC1 {:n-channels 1
                        :datatype :int32}
   opencv_core/CV_32SC2 {:n-channels 2
                        :datatype :int32}
   opencv_core/CV_32SC3 {:n-channels 3
                        :datatype :int32}
   opencv_core/CV_32SC4 {:n-channels 4
                         :datatype :int32}

   opencv_core/CV_32FC1 {:n-channels 1
                        :datatype :float32}
   opencv_core/CV_32FC2 {:n-channels 2
                        :datatype :float32}
   opencv_core/CV_32FC3 {:n-channels 3
                        :datatype :float32}
   opencv_core/CV_32FC4 {:n-channels 4
                         :datatype :float32}

   opencv_core/CV_64FC1 {:n-channels 1
                        :datatype :float64}
   opencv_core/CV_64FC2 {:n-channels 2
                        :datatype :float64}
   opencv_core/CV_64FC3 {:n-channels 3
                        :datatype :float64}
   opencv_core/CV_64FC4 {:n-channels 4
                         :datatype :float64}})


(def channels-datatype->opencv-type-map
  (c-set/map-invert opencv-type->channels-datatype-map))


(defn acceptable-image-params?
  [datatype shape]
  (and (sequential? shape)
       (= 3 (count shape))
       (let [[height width chans] shape
             opencv-code (get channels-datatype->opencv-type-map
                              {:n-channels chans
                               :datatype datatype})]
         (boolean opencv-code))))


(defmacro thrownil
  [x message map]
  `(if-let [x# ~x]
     x#
     (throw (ex-info ~message ~map))))


(defn opencv-type->channels-datatype
  "Given an opencv type map to
  {:n-channels num-channels
  :datatype datatype}"
  [opencv-type]
  (thrownil (get opencv-type->channels-datatype-map opencv-type)
            "Failed to map from opencv type to channels and datatype"
            {:opencv-type opencv-type}))


(defn channels-datatype->opencv-type
  "Map from n-channels and datatype -> opencv type"
  ^long [n-channels datatype]
  (thrownil (get channels-datatype->opencv-type-map {:n-channels n-channels
                                                     :datatype datatype})
            "Failed to map from chanels datatype -> opencv type"
            {:n-channels n-channels
             :datatype datatype}))

(declare new-mat)


(extend-type opencv_core$Mat
  stack/PResource
  (release-resource [item]
    (.release item)
    (.deallocate item))

  dtype-proto/PShape
  (shape [m] [(.rows m) (.cols m) (.channels m)])

  dtype-proto/PCountable
  (ecount [m] (* (.rows m) (.cols m) (.channels m)))

  dtype-proto/PDatatype
  (get-datatype [m] (-> (.type m)
                        opencv-type->channels-datatype
                        :datatype))

  dtype-proto/PClone
  (clone [m]
    (let [[height width channels] (dtype/shape m)]
      (new-mat height width channels :dtype (dtype/get-datatype m))))

  dtype-proto/PPrototype
  (from-prototype [item datatype shape]
    (if (acceptable-image-params? datatype shape)
      (let [[height width channels] shape]
        (new-mat height width channels :dtype datatype))
      (dtype/make-container :native-buffer datatype (apply * 1 shape))))

  dtype-proto/PToBufferDesc
  (convertible-to-buffer-desc? [item] true)
  (->buffer-descriptor [item]
    (let [datatype (dtype/get-datatype item)
          shape (dtype/shape item)]
      {:datatype datatype
       :ptr (jna/->ptr-backing-store item)
       :shape shape
       :strides (->> (dims-analytics/shape-ary->strides (dtype/shape item))
                     (mapv (partial * (casting/numeric-byte-width datatype))))}))


  jcpp-dtype/PToPtr
  (convertible-to-javacpp-ptr? [item] true)
  (->javacpp-ptr [item] (.ptr item))


  dtype-proto/PToJNAPointer
  (convertible-to-data-ptr? [item] true)
  (->jna-ptr [item]
    (let [retval (jna/as-ptr
                  (.ptr item))
          src-map {:item-ptr item}]
      (resource/track retval #(get src-map :item-ptr) [:gc]))))


(defn new-mat
  ^opencv_core$Mat [height width n-channels & {:keys [dtype]
                                               :or {dtype :uint8}}]
  (resource/track
   (opencv_core$Mat. (int height)
                     (int width)
                     (channels-datatype->opencv-type
                      n-channels dtype))))


(defn load
  "Note you can call tech.v2.datatype/shape and tech.v2.datatype/get-datatype
  to figure out what was loaded."
  ^opencv_core$Mat [^String path]
  (resource/track (opencv_imgcodecs/imread path)))


(defn save
  [^opencv_core$Mat img ^String path]
  (opencv_imgcodecs/imwrite path img)
  img)


(def resize-algo-kwd->opencv-map
  {;;Bilinear
   :linear opencv_imgproc/CV_INTER_LINEAR
   :bilinear opencv_imgproc/CV_INTER_LINEAR
   ;;Cubic
   :cubic opencv_imgproc/CV_INTER_CUBIC
   ;;Pixel area averaging
   :area opencv_imgproc/CV_INTER_AREA
   ;;Lanczos with a 4x4 filter
   :lanczos opencv_imgproc/CV_INTER_LANCZOS4
   ;;Nearest Neighbor
   :nn opencv_imgproc/CV_INTER_NN
   :nearest opencv_imgproc/CV_INTER_NN})


(defn resize-algo-kwd->opencv
  ^long [resize-algo]
  (thrownil (get resize-algo-kwd->opencv-map resize-algo)
            "Failed to map resize algo to opencv"
            {:resize-algorithm resize-algo}))


(defn size
  ^opencv_core$Size [width height]
  (opencv_core$Size. (int width) (int height)))


(defn resize-imgproc
  "Use improc resize method directly."
  [^opencv_core$Mat src-img
   ^opencv_core$Mat dest-img
   resize-algorithm-kwd]
  (let [[new-height new-width n-chans] (dtype/shape dest-img)
        new-width (int new-width)
        new-height (int new-height)]
    (opencv_imgproc/resize src-img dest-img
                           (size new-width new-height)
                           0.0 0.0
                           (resize-algo-kwd->opencv resize-algorithm-kwd))))


(defn resize
  "Resize the source image producing a new image."
  ([src-img new-width new-height {:keys [resize-algorithm] :as options}]
   (let [[src-height src-width n-channels] (dtype/shape src-img)
         retval (new-mat new-height new-width n-channels
                         :dtype (dtype/get-datatype src-img))
         resize-algorithm (or resize-algorithm
                              (if (> (int new-width)
                                     (int src-width))
                                :linear
                                :area))]
     (resize-imgproc src-img retval resize-algorithm)
     retval))
  ([src-img new-width new-height]
   (resize src-img new-width new-height {}))
  ([src-img new-width]
   (let [[src-height src-width chans] (dtype/shape src-img)
         ratio (/ (double new-width) (double src-width))
         new-height (-> (* (double src-height) ratio)
                        Math/round
                        long)]
     (resize src-img new-width new-height))))


(defn clone
  [src-img]
  (dtype/clone src-img))


(defmethod dtype-proto/make-container :opencv-image
  [container-type datatype img-shape options]
  (when-not (acceptable-image-params? datatype img-shape)
    (throw (ex-info
            (format "Cannot create image of datatype/shape combination: %s/%s"
                    datatype img-shape)
            {})))
  (let [[height width channels] img-shape]
    (new-mat height width channels :dtype datatype)))
