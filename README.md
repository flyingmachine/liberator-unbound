# Liberator Unbound

Liberator Unbound lets you create functions which generate liberator
resources. This makes it possible for you to create configurable
Liberator libraries (as opposed to using `defresource`, which creates
resources that are set in stone). For example, I've used it to create
a Clojure + Datomic forum library that allows developers to customize
things like what to do after a user creates a new post or topic (send
an email, send a slack notification, etc).

As an added bonus, Liberator Unbound works great with
[Stuart Sierra's component library](https://github.com/stuartsierra/component).
Component is all about passing in stateful dependencies as arguments
to functions, and Liberator Unbound is all about generating resources
using functions that take arguments. Hooray!


## Usage

If you're a *just gimme the source code* kind of person, have a look
at [the tests](test/com/flyingmachine/liberator_unbound_test.clj).

Creating resources is a multi-step process of building up a decision
map to pass to Liberator's `resource` function. First, you create a
function that takes one argument and generates resource decisions:

```clojure
(defn resource-decisions
  [options]
  {:list   {:handle-ok (fn [_] (-> options :list :data))}
   :create {:handle-created (fn [_] (-> options :create :data))}
   :show   {:handle-ok (fn [_] (-> options :show :data))}
   :update {:handle-ok (fn [_] (-> options :update :data))}
   :delete {:handle-deleted (fn [_] (-> options :delete :data))}})
```

In this case, `:list`, `:create` and the other keys are all completely
arbitrary. Next, you apply this function to options to produce the
resource decisions:

```clojure
;; here are the options
(def options
  {:list   {:data "list data"}
   :create {:data "create data"}
   :show   {:data "show data"}
   :update {:data "update data"}
   :delete {:data "delete data"}})

;; now generate decisions
(resource-decisions options)
;; This effectively returns
{:list   {:handle-ok (fn [_] "list data")}
 :create {:handle-created (fn [_] "create data")}
 :show   {:handle-ok (fn [_] "show data")}
 :update {:handle-ok (fn [_] "update data")}
 :delete {:handle-deleted (fn [_] "delete data")}}
```

In real life, `options` would be something like

```clojure
{:create {:after-create (fn [] "do-thing-after-create")}}
```

In the forum I mentioned earlier, this is what lets me send out an
email or post to slack or whatever I want.

You can merge this result with a map of default decisions, allowing
you to eliminate repetition. Here's some example code for setting JSON
defaults:

```clojure
(defn record-in-ctx
  [ctx]
  (:record ctx))

(defn errors-in-ctx
  ([]
   (errors-in-ctx {}))
  ([opts]
   (fn [ctx]
     (merge {:errors (:errors ctx)} opts))))

(def json
  ^{:doc "A 'base' set of liberator resource decisions for list,
    create, show, update, and delete"}
  (let [errors-in-ctx (errors-in-ctx {:representation {:media-type "application/json"}})
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
```

`record-in-ctx` and `errors-in-ctx` are just a couple convenience
functions. The real work is below. You can see that each of `:list`,
`:create`, and the other decision maps set sensible values for the
methods they allow, the media type, and so forth.

You can merge these defaults with your custom resource decisions using
`merge-decisions`. This returns a nested map that's too large to show
all of. Here's the effective value of the `:delete` key so you can get
an idea of what's going on:

```clojure
(:delete (merge-decisions json (resource-decisions options)))
; =>
{:handle-deleted (fn [_] "delete data")
 :available-media-types ["application/json"]
 :allowed-methods [:delete]
 :authorized? true
 :handle-unauthorized (errors-in-ctx {:representation {:media-type "application/json"}})
 :handle-malformed (errors-in-ctx {:representation {:media-type "application/json"}})
 :respond-with-entity? false,
 :new? false}
```

At this point, you have full decision maps that are ready to be turned
into actual resources - functions which can serve as Ring
handlers. You can do this with `resources`:

```clojure
(resources {:collection [:list :create]
            :entry [:show :update :delete]}
           (merge-decisions json (resource-decisions options)))
```

This returns a map with two keys, `:collection` and `:entry`. The
values are liberator resources which dispatch based on a request's
method. For example, the value for `:collection` is a resource that
uses the `:list` decisions if the request's method is GET, and uses
the `:create` decisions if the request's method is POST. Likewise, the
value of `:entry` is a resource that dispatches to the `:show`
decisions if the method is GET, `:update` for PUT, and `:delete` for
DELETE.

There's one more function, `resource-route`, which creates Compojure
routes. Here's how you'd call it:

```clojure
(def my-resources
  (resources {:collection [:list :create]
              :entry [:show :update :delete]}
             (merge-decisions json (resource-decisions options))))

(resource-route "/my-resources" my-resources {:entry-key ":id"})
```

This is the equivalent of calling:

```clojure
(compojure.core/routes
 (compojure.core/ANY "/my-resources" [] (:collection my-resources))
 (compojure.core/ANY "/my-resources/:id" [] (:entry my-resources)))
```

Lastly, the `bundle` function returns a function that lets you combine
everything neatly. Here's a real-world example. Note that I'm
requiring the libraries now, which I wasn't doing earlier:

```clojure
(require '[com.flyingmachine.liberator-unbound :as lu])
(require '[com.flyingmachine.liberator-unbound.default-decisions :as lud])

(def resource-route (lu/bundle {:collection [:list :create]
                                :entry [:show :update :delete]}
                               lud/json))
(defn build-core-routes
  "This provides an easy way to customize the options for
  resources. For more extensive customization, you'll need to write
  things out in your host project."
  [app-config]
  (compojure.core/routes
   (resource-route "/posts" posts/resource-decisions app-config)
   (resource-route "/topics" topics/resource-decisions app-config)))
```

`build-core-routes` creates four routes, one each for `/posts`,
`/posts/:id`, `/topics`, `/topics/:id`. This works great with
Component - routes and ring handlers are all created with functions.

## License

Copyright © 2015 Daniel Higginbotham

Distributed under the MIT License
