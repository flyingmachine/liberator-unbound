(defproject com.flyingmachine.liberator-unbound "0.1.0-SNAPSHOT"
  :description "A library for creating liberator libraries"
  :url "https://github.com/flyingmachine/liberator-unbound"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :profiles {:def {:dependencies [[liberator "0.12.1"]
                                  [ring "1.3.0"]]}})
