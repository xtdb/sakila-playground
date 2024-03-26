;; Copyright © 2024, JUXT LTD.

(ns xtdb.demo.adoc.resources
  {:web-context "/adoc/"}
  (:require
   [xtdb.demo.web.resource :refer [map->Resource html-resource html-templated-resource]]
   [xtdb.api :as xt]
   [xtdb.node :as xtn]
   [clojure.java.io :as io]
   [clojure.reflect :as reflect]
   [clojure.string :as str])
  (:import
   (org.asciidoctor.extension ExtensionGroup BlockMacroProcessor BlockProcessor)
   (org.asciidoctor Asciidoctor Options)))

;; See https://docs.asciidoctor.org/asciidoctorj/latest/extensions/ast-introduction/

;; http://localhost:3010/adoc/tutorial_part_1

(set! *warn-on-reflection* true)


(def raw-adoc (org.asciidoctor.Asciidoctor$Factory/create))

(defn parse-date [s]
  (java.util.Date/from
   (.toInstant (.atZone (.atStartOfDay (java.time.LocalDate/parse s)) (java.time.ZoneId/of "Z")))))

(defn extend-adoc ^org.asciidoctor.Asciidoctor [^org.asciidoctor.Asciidoctor adoc xt-node]
  (let [current-time (atom (java.util.Date.))]
    (doto adoc
      (..
       (createGroup "my-extensions")

       (blockMacro
        (proxy [BlockMacroProcessor] ["xt"]
          (process [^org.asciidoctor.ast.StructuralNode parent target attributes]
            (let [this ^BlockMacroProcessor this]
              (case target
                "set-simulation-time"
                (let [date (parse-date (get attributes "inst"))]
                  (reset! current-time date)
                  (proxy-super createBlock parent "paragraph" (format "Setting simulation time: `%s`" date)))
                (proxy-super
                 createBlock parent "listing"
                 (format "Unknown block macro processor target: '%s' %s" target (type target))))))))

       (block
        (proxy [BlockProcessor] ["xtsubmit"]
          (process [^org.asciidoctor.ast.StructuralNode parent
                    ^org.asciidoctor.extension.Reader reader
                    attributes]
            (let [this ^BlockProcessor this
                  sql (.read reader)]
              (xt/submit-tx xt-node [[:sql sql]] {:system-time @current-time})
              (proxy-super
               createBlock parent "listing"
               sql
               )))))

       (block
        (proxy [BlockProcessor] ["xtquery"]
          (process [^org.asciidoctor.ast.StructuralNode parent
                    ^org.asciidoctor.extension.Reader reader
                    attributes]
            (let [this ^BlockProcessor this
                  sql (.read reader)
                  rs (xt/q xt-node sql {:current-time @current-time
                                        :key-fn :snake-case-string})]


              #_(.load adoc
                       (str/join "\n" ["----" sql "----" ".Results" "----"
                                       (str/join "\n" (for [row rs] (pr-str row)))
                                       "----"
                                       (type parent)]



                                 )
                       (.. (Options/builder)
                           (build)))
              (let [structure
                    [:section {:title "SQL listing"}
                     [:listing {} "foo"]]

                    #_generator
                    #_(fn gen [root parent [k atts & content]]
                        (let [block
                              (case k
                                :section (let [block
                                               (cond-> (proxy-super
                                                        createSection root
                                                        false {})
                                                 (:title atts) (.setTitle (:title atts))

                                                 )]
                                           block
                                           )
                                :listing (proxy-super
                                          createBlock root "listing"
                                          "sql"
                                          ))]
                          (println "content is" (pr-str content))
                          (doseq [child content
                                  :when (vector? child)]
                            (println "appending to parent" parent "child" child)
                            (.append root (gen root parent child))
                            )
                          ))]

                #_(generator parent parent structure)

                (let [^org.asciidoctor.ast.Section section
                      (proxy-super
                       createSection parent
                       false {})

                      _ (.setTitle section "SQL listing")

                      ^org.asciidoctor.ast.Block listing
                      (proxy-super
                       createBlock section "listing"
                       sql
                       )
                      _ (.setTitle listing "listing-title")
                      _ (.append section listing)

                      ^org.asciidoctor.ast.Block
                      results-para
                      (proxy-super
                       createBlock section "paragraph"
                       "and the results are:")
                      _ (.append section results-para)

                      ^org.asciidoctor.ast.Table
                      table
                      (proxy-super createTable section)

                      ^org.asciidoctor.ast.Column
                      column (proxy-super createTableColumn table 0 {})
                      _ (.add (.getColumns table) column)

                      ^org.asciidoctor.ast.Row
                      row
                      (proxy-super createTableRow table)

                      _ (println (.getBody table) (type (.getBody table)))
                      _ (.add (.getBody table) row)

                      ;;_ (println (.getBody table))
                      _ (println "row" row (type row))
                      ;; _ (.clear (.getBody table))

                      ;;_ (.set (.getBody table) 0 row)

                      ;;_ (.append results-table row)
                      ;;_ (.append section table)
                      ]

                  section))))))

       register))))

;;(reflect/reflect org.asciidoctor.jruby.ast.impl.TableImpl$RowList)
;;(reflect/reflect org.asciidoctor.jruby.ast.impl.RowImpl)

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
