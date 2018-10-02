# tech.opencv

[![Clojars Project](https://clojars.org/techascent/tech.opencv/latest-version.svg)](https://clojars.org/techascent/tech.opencv)

OpenCV bindings via javacpp.

## Usage
Load/save, resize, clone.  Becase opencv matrixes need to be released and thus aren't garbage collected they are bound to a think.resource.core/resource-context which unwinds in a way similar to C++ RAII or with-open.

Bindings to clojure.core.matrix and tech.datatype allow you to get into the properties of
a loaded matrix.

Please refer to the [tests](test/tech/opencv_test.clj).

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
