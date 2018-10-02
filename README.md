# tech.opencv

[![Clojars Project](https://clojars.org/techascent/tech.opencv/latest-version.svg)](https://clojars.org/techascent/tech.opencv)

OpenCV bindings via javacpp.

## Usage
Load/save, resize, clone.  Becase opencv matrixes need to be released and thus aren't garbage collected they are bound to a think.resource.core/resource-context which unwinds in a way similar to C++ RAII or with-open.

Bindings to clojure.core.matrix and tech.datatype allow you to get into the properties of
a loaded matrix.

```clojure
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
                        (map convert-fn (take 10 float-data))))))
```

Please refer to the [tests](test/tech/opencv_test.clj) and [opencv.clj](src/tech/opencv.clj).


## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
