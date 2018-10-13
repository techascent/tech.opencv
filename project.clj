(defproject techascent/tech.opencv "0.2.2"
  :description "Opencv bindings via javacpp"
  :url "http://github.com/tech-ascent/tech.opencv"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [techascent/tech.javacpp-datatype "0.5.5"]
                 [org.bytedeco.javacpp-presets/opencv-platform "3.4.0-1.4"]]

  ;;Makes for some great demos

  :profiles {:dev {:dependencies [[techascent/tech.compute "0.6.6"]]}})
