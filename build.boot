(set-env!
 :source-paths   #{"src"}
 :resource-paths    #{"html"}
 :dependencies '[[adzerk/boot-cljs      "0.0-3308-0" :scope "test"]
                 [adzerk/boot-cljs-repl "0.1.10-SNAPSHOT" :scope "test"]
                 [adzerk/boot-reload    "0.2.6"      :scope "test"]
                 [boot-cljs-test/node-runner "0.1.0" :scope "test"]
                 [cpmcdaniel/boot-copy "1.0" :scope "provided"]
                 [org.clojure/clojurescript "0.0-3308"  :scope "test"]
                 [adzerk/boot-logservice "1.0.1" scope "test"]
                 [re-frame "0.4.1"]
                 [com.cognitect/transit-cljs "0.8.220"]
                 [com.taoensso/encore "1.37.0"]
                 [reagent "0.5.0"]
                 [reagent-utils "0.1.4"]
                 [markdown-clj "0.9.66"]
                 [prismatic/schema "0.4.3"]
                 [com.taoensso/sente "1.6.0-alpha1"]
                 [hiccups "0.3.0"]
                 ])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[boot-cljs-test.node-runner :refer :all]
 '[adzerk.boot-reload    :refer [reload]]
 '[cpmcdaniel.boot-copy :refer :all])

(task-options!
 copy {:output-dir    "."
       :matching       #{#"\.js$"}})


(deftask dev []
  (set-env! :source-paths #{"src" "test" })
  (comp (watch)
        (speak)
        (reload :on-jsload 'de.prob2.routing/init!)
        (cljs-repl)
        (cljs-test-node-runner
           :namespaces '[de.prob2.other-unicode-tests
                         de.prob2.ascii-tounicode
                         de.prob2.unicode-toascii])
        (cljs :source-map true
              :optimizations :none)
        (run-cljs-test)
        ))

(deftask build []
  (set-env! :source-paths #{"src"})
  (comp (cljs :optimizations :simple
              :compiler-options {:target :nodejs
                                 :output-dir "../tmp/"
                                 :output-to "./lib/prob_ui.js"})
        (copy)))
