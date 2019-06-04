(defproject com.flyingmachine/liberator-unbound "0.2.0"
  :description "A library for creating liberator libraries"
  :url "https://github.com/flyingmachine/liberator-unbound"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:dev {:dependencies [[liberator "0.15.3"]
                                  [compojure "1.1.8"]
                                  [ring "1.3.0"]
                                  [ring-mock "0.1.5"]]}})
