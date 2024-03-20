;; Copyright © 2024, JUXT LTD.

(ns xtdb.demo.adoc.play
  (:require
   [clojure.test :as t]
   [clojure.reflect :as reflect])
  (:import
   (org.asciidoctor.extension ExtensionGroup BlockMacroProcessor)
   (org.asciidoctor Asciidoctor Options)))



(let [adoc (Asciidoctor$Factory/create)
      extension-group
      (.. adoc
          (createGroup "my-extensions")
          (blockMacro
           (proxy [BlockMacroProcessor] []
             (process [parent target attributes]
               (proxy-super createBlock parent "pass" "working!"))
             (getName [] "sql")))
          register)
      options
      (.. (Options/builder)
          ;;(backend "docbook")
          (build))]

  (.convert adoc
            "Hello World!

sql::mygithubaccount/8810011364687d7bec2c[a=b]
"
            options
            )

  )
