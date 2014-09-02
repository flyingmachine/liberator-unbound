(ns com.flyingmachine.liberator-unbound.default-decisions
  "Some useful defaults for liberator unbound")

(defn record-in-ctx
  [ctx]
  (:record ctx))

(def json
  ^{:doc "A 'base' set of liberator resource decisions for list,
    create, show, update, and delete"}
  (let [errors-in-ctx (fn [ctx]
                        {:errors (:errors ctx)
                         :representation {:media-type "application/json"}})
        
        base {:available-media-types ["application/json"]
              :allowed-methods [:get]
              :authorized? true
              :handle-unauthorized errors-in-ctx
              :handle-malformed errors-in-ctx
              :respond-with-entity? true
              :new? false}]
    {:list base
     :create (merge base {:allowed-methods [:post]
                          :new? true
                          :handle-created record-in-ctx})
     :show base
     :update (merge base {:allowed-methods [:put]})
     :delete (merge base {:allowed-methods [:delete]
                          :respond-with-entity? false})}))
