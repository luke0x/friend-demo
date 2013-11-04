(ns cemerick.friend-demo
  (:require [cemerick.friend-demo.misc :as misc]
            (compojure handler [route :as route])
            [compojure.core :as compojure :refer (GET defroutes)]
            [hiccup.core :as h]
            [hiccup.element :as e]
            [ring.middleware.resource :refer (wrap-resource)]
            ring.adapter.jetty
            [bultitude.core :as b]))

(defn- demo-vars
  [ns]
  {:namespace ns
   :ns-name (ns-name ns)
   :name (-> ns meta :name)
   :doc (-> ns meta :doc)
   :route-prefix (misc/ns->context ns)
   :app (ns-resolve ns 'app)
   :page (ns-resolve ns 'page)})

(def the-menagerie (->> (b/namespaces-on-classpath :prefix misc/ns-prefix)
                        distinct
                        (map #(do (require %) (the-ns %)))
                        (map demo-vars)
                        (filter #(or (:app %) (:page %)))
                        (sort-by :ns-name)))

(defn- wrap-app-metadata
  [h app-metadata]
  (fn [req] (h (assoc req :demo app-metadata))))

(def site
  (apply compojure/routes
    (route/resources "/" {:root "META-INF/resources/webjars/foundation/4.0.4/"})
    (for [{:keys [app page route-prefix] :as metadata} the-menagerie]
      (compojure/context route-prefix []
        (wrap-app-metadata (compojure/routes (or page (fn [_])) (or app (fn [_]))) metadata)))))