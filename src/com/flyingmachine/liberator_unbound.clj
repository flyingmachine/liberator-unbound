(ns com.flyingmachine.liberator-unbound
  "Functions to more easily define resources at runtime"
  (:require [liberator.core :refer (resource default-functions)]))

(defn method-resource-map
  "Associates HTTP methods with the resource definition"
  [config-map]
  (reduce (fn [result [k v]]
            (assoc result (or (first (:allowed-methods v)) :get) k))
          {}
          config-map))

(defn method-dispatcher
  "Returns a function which calls the correct option based on the
  request method"
  [config-map method-map option]
  (fn [ctx]
    (let [request-method (get-in ctx [:request :request-method])
          resource-key (request-method method-map)
          entry (get-in config-map [resource-key option])]
      (cond
       (fn? entry) (entry ctx)
       (nil? entry) (option default-functions)
       :else entry))))

(defn combine-configs
  [config-map]
  (let [configs (vals config-map)
        options (disj (set (mapcat keys configs)) :allowed-methods)
        seed {:allowed-methods (set (mapcat :allowed-methods configs))}
        method-map (method-resource-map config-map)]
    (reduce (fn [config option]
              (assoc config option (method-dispatcher config-map method-map option)))
            seed
            options)))

(defn config->resource
  [config]
  (apply resource (apply concat config)))

(defn resource-for-keys
  [resource-configs keys]
  (let [resource-config (select-keys resource-configs keys)
        config-count (count resource-config)]
    (cond
     (= 0 config-count) nil
     (= 1 config-count) (config->resource (first (vals resource-config)))
     :else (config->resource (combine-configs resource-config)))))

(defn entry-resource
  [resource-configs]
  (resource-for-keys resource-configs :show :update :delete))

(defn collection-resource
  [resource-configs]
  (resource-for-keys resource-configs :list :create))

(defn merge-decision-defaults
  "This allows you to define your resource configurations more
  compacatly by merging them with a map of decision defaults"
  [defaults decisions]
  (merge-with merge
              (select-keys defaults (keys decisions))
              decisions))

(defn resource-config
  [decision-defaults resource-config-creator opts]
  (merge-decision-defaults decision-defaults (resource-config-creator opts)))

(defn resources
  "e.g. (resources {} {:collection [:list :create]}"
  [groups config]
  (into {} (map (fn [[group-name config-keys]]
                  [group-name (resource-for-keys config config-keys)])
                groups)))
