(defproject techascent/tech.opencv "2.0"
  :description "Opencv bindings via javacpp"
  :url "http://github.com/tech-ascent/tech.opencv"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [techascent/tech.javacpp-datatype "2.2"]
                 [org.bytedeco.javacpp-presets/opencv-platform "3.4.0-1.4"]]

  ;;Makes for some great demos
  :profiles {:dev {:dependencies [[techascent/tech.compute "2.0"]]}})
