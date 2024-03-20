;; Copyright © 2024, JUXT LTD.

(ns xtdb.demo.adoc.resources
  {:web-context "/adoc/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource]]
   [xtdb.api :as xt]
   [xtdb.node :as xtn]
   [clojure.java.io :as io])
  (:import
   (org.asciidoctor.extension ExtensionGroup BlockMacroProcessor)
   (org.asciidoctor Asciidoctor Options)))

;; http://localhost:3010/adoc/tutorial_part_1

(defonce raw-adoc (org.asciidoctor.Asciidoctor$Factory/create))

(defn extend-adoc [adoc xt-node]
  (doto adoc
    (.. (createGroup "my-extensions")
        (blockMacro
         (proxy [BlockMacroProcessor] []
           (process [parent target attributes]
             (case target
               "set-simulation-time"
               (proxy-super createBlock parent "pass" (format "Setting simulation time to %s" (pr-str attributes)))
               (proxy-super
                createBlock parent "pass"
                (format "Unknown block macro processor target: '%s' %s" target (type target)))))

           (getName [] "xt")))
        register)))

#_(.. (Options/builder)
      ;;(backend "docbook")
      (build))

#_(.convert adoc
            "Hello World!

sql::mygithubaccount/8810011364687d7bec2c[a=b]
"
            options
            )


(defn ^{:uri-template "{name}"
        :uri-variables {:name :string}}
  adoc-notebook
  [{:keys [name]}]
  (let [xt-node (xtn/start-node {})]
    (map->Resource
     {:representations
      [^{"content-type" "text/html;charset=utf-8"}
       (fn [req]
         {:body
          (.convert
           (extend-adoc raw-adoc xt-node)
           (slurp (io/file (format "dev/nb/%s.adoc" name)))

           (.. (Options/builder)
               ;;(backend "docbook")
               (build)))
          })]})))
