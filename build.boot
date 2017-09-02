(set-env!
 :source-paths #{"build/src"}
 :resource-paths #{"build/resources"})

(require '[pleasetrythisathome.build :refer :all])

(def org "myblackdoor")
(def project "blackdoor-flow-charts")
(def description "Blackdoor flow charts")
(def version (deduce-version-from-git))
(def repl-port 5600)

(merge-project-env! (project-env))

(doseq [dir (submodules)]
  (merge-project-env! (project-env dir) true))

(merge-env!
 :dependencies (->> [{:boot [:laces]}]
                    (pull-deps)
                    (scope-as "test")))

(require
 '[adzerk.bootlaces :refer :all])

(bootlaces! version)

(task-options!
 pom {:project (symbol org project)
      :version version
      :description description
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}
      :url (format "https://github.com/%s/%s" org project)
      :scm {:url (format "https://github.com/%s/%s" org project)}}
 repl {:port repl-port})

(deftask dev
  []
  (safe-merge-paths! :source-paths #{"dev"})
  (comp
   (watch)
   (notify)
   (repl :server true)
   (target)))
