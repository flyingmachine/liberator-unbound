(ns com.flyingmachine.liberator-unbound-test
  (:require [com.flyingmachine.liberator-unbound :as lu]
            [com.flyingmachine.liberator-unbound.default-decisions :as lud]
            [ring.mock.request :refer :all])
  (:use midje.sweet))

(defn resource-config-creator
  [options]
  {:list   {:handle-ok (fn [_] (-> options :list :data))}
   :create {:handle-created (fn [_] (-> options :create :data))}
   :show   {:handle-ok (fn [_] (-> options :show :data))}
   :update {:handle-ok (fn [_] (-> options :update :data))}
   :delete {:handle-deleted (fn [_] (-> options :delete :data))}})

(def options
  {:list   {:data "list data"}
   :create {:data "create data"}
   :show   {:data "show data"}
   :update {:data "update data"}
   :delete {:data "delete data"}})

(def resources (lu/resources {:collection [:list :create]
                              :entry [:show :update :delete]}
                             (lu/resource-config lud/json resource-config-creator options)))

(fact "resource groups dispatch requests correctly"
  (let [handler (:collection resources)]
    (fact "list dispatches correctly"
      (handler (request :get "/"))
      => {:body "list data"
          :headers {"Content-Type" "application/json;charset=UTF-8"
                    "Vary" "Accept"}
          :status 200})
    (fact "create dispatches correctly"
      (handler (request :post "/"))
      => {:body "create data"
          :headers {"Content-Type" "application/json;charset=UTF-8"
                    "Vary" "Accept"}
          :status 201})
    (fact "returns method not allowed invalid HTTP method"
      (handler (request :put "/"))
      => {:body "Method not allowed."
          :headers {"Allow" "GET, POST"
                    "Content-Type" "text/plain"}
          :status 405}))

  (let [handler (:entry resources)]
    (fact "show dispatches correctly"
      (handler (request :get "/"))
      => {:body "show data"
          :headers {"Content-Type" "application/json;charset=UTF-8"
                    "Vary" "Accept"}
          :status 200})
    (fact "update dispatches correctly"
      (handler (request :put "/"))
      => {:body "update data"
          :headers {"Content-Type" "application/json;charset=UTF-8"
                    "Vary" "Accept"}
          :status 200})
    (fact "delete dispatches correctly"
      (handler (request :delete "/"))
      => {:body nil
          :headers {"Content-Type" "text/plain"}
          :status 204})))
