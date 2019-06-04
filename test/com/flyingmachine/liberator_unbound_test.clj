(ns com.flyingmachine.liberator-unbound-test
  (:require [com.flyingmachine.liberator-unbound :as lu]
            [com.flyingmachine.liberator-unbound.default-decisions :as lud]
            [ring.mock.request :refer :all]
            [clojure.test :refer :all]))

(def resource-decisions
  {:list   {:handle-ok (fn [_] "list data")}
   :create {:handle-created (fn [_] "create data")}
   :show   {:handle-ok (fn [_] "show data")}
   :update {:handle-ok (fn [_] "update data")}
   :delete {:handle-deleted (fn [_] "delete data")}})

(def handler
  (lu/resource-route
    "/test"
    (lu/resources {:collection [:list :create]
                   :entry [:show :update :delete]}
                  (lu/merge-decisions lud/json resource-decisions))))

(def resource-bundle (lu/bundle lu/resource-groups lud/json))

(def bundled-handler (resource-bundle "/test" resource-decisions))

(deftest handlers
  (testing "resource groups dispatch requests correctly"
    (is (= {:body    "list data"
            :headers {"Content-Type" "application/json;charset=UTF-8"
                      "Vary"         "Accept"}
            :status  200}
           (handler (request :get "/test"))
           (bundled-handler (request :get "/test"))))

    
    (is (= {:body    "create data"
            :headers {"Content-Type" "application/json;charset=UTF-8"
                      "Vary"         "Accept"}
            :status  201}
           (handler (request :post "/test"))
           (bundled-handler (request :post "/test"))))

    (is (= {:body    "show data"
            :headers {"Content-Type" "application/json;charset=UTF-8"
                      "Vary"         "Accept"}
            :status  200}
           (handler (request :get "/test/x"))
           (bundled-handler (request :get "/test/x"))))
    
    (is (= {:body    "update data"
            :headers {"Content-Type" "application/json;charset=UTF-8"
                      "Vary"         "Accept"}
            :status  200}
           (handler (request :put "/test/x"))
           (bundled-handler (request :put "/test/x"))))
    
    (is (= {:body    nil
            :headers {"Content-Type" "text/plain"}
            :status  204}
           (handler (request :delete "/test/x"))
           (bundled-handler (request :delete "/test/x"))))))

(deftest invalid-stuff
  (testing "returns method not allowed invalid HTTP method"
    (is (= {:body    "Method not allowed."
            :headers {"Allow"        "GET, POST"
                      "Content-Type" "text/plain"}
            :status  405}
           (handler (request :put "/test"))
           (handler (request :delete "/test"))
           (bundled-handler (request :put "/test"))
           (bundled-handler (request :delete "/test"))))))
