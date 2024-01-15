(ns yry.repl)

(comment 
  
  "Portal"
  
  (require '[portal.api :as p])
  (def p (p/open {:launcher :vs-code}))
  
  (add-tap #'p/submit)
  
  (tap> 1)
  )

(comment 
  "Query helplers"
  
  (require '[xtdb.api :as xt])
  
  (defn tap>query
    [xt-node query & args]
    (-> xt-node
        :xt-node
        (xt/q query args)
        (with-meta {:portal.viewer/default :portal.viewer/table})
        tap>))
  
  )