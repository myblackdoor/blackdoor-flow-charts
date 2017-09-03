(ns blackdoor.flow-charts.alpha
  (:require [clojure.string :as str]
            [me.raynes.fs :as fs]
            [plumbing.core :refer :all]
            [rhizome.viz :as viz]
            [taoensso.timbre :as log]
            [clojure.set :as set]))

(def flows
  {:public
   {:com/myblackdoor [:blackdoor/member
                      :blackdoor/guest]
    :blackdoor/guest [:typeform/subscribe]
    :blackdoor/member [:slack/signin
                       :loomio/signin]
    :loomio/signin [:loomio.subgroups/blackdoor]
    :slack/signin [:slack/commands]
    :facebook/myblackdoor [:com/myblackdoor]
    :instagram/myblackdoor [:com/myblackdoor]}
   :subscribe
   {:typeform/subscribe [:emails/subscribed
                         :mailcimp/blackdoor
                         :patreon/blackdoor]
    :emails/subscribed [:patreon/blackdoor]}
   :invite
   {:typeform/invite [:emails/invite]
    :emails/invite [:typeform/apply]}
   :apply
   {:typeform/apply [:emails/applied
                     :loomio.subgroups/applications]
    :emails/applied [:patreon/blackdoor]
    :loomio.subgroups/applications [:loomio.subgroups.applications/approve
                                    :loomio.subgroups.applications/reject]
    :loomio.subgroups.applications/approve [:emails/accepted]
    :emails/accepted [:typeform/signup]
    :loomio.subgroups.applications/reject [:emails/rejected]
    :emails/rejected []}
   :signup
   {:typeform/signup [:docs/terms-of-service]
    :docs/terms-of-service [:slackpass/signup]
    :slackpass/signup [:emails/signed-up
                       :docs/orientation
                       :loomio/new-user
                       :slack/new-user
                       :trello/new-user
                       :google-drive/share]
    :emails/signed-up [:docs/orientation
                       :calendly.schedule/onboarding
                       :patreon/blackdoor]
    :calendly.schedule/onboarding []
    :loomio/new-user []
    :slack/new-user []
    :trello/new-user []
    :google-drive/share []}
   :docs
   {:google-drive/blackdoor [:docs/orientation
                             :docs/guidelines
                             :docs/user-guide
                             :docs/faq]
    :docs/orientation [:calendly.schedule/onboarding
                       :docs/guidelines
                       :docs/user-guide
                       :docs/faq
                       :patreon/blackdoor]
    :docs/guidelines []
    :docs/faq []
    :docs/user-guide []}
   :moderation
   {:typeform/report [:loomio.subgroups/moderation]
    :loomio.subgroups/moderation [:loomio.subgroups.moderation/allow
                                  :loomio.subgroups.moderation/reprimand
                                  :loomio.subgroups.moderation/ban]
    :loomio.subgroups.moderation/allow []
    :loomio.subgroups.moderation/reprimand [:emails/reprimand]
    :emails/reprimand [:calendly.schedule/talk]
    :loomio.subgroups.moderation/ban [:emails/ban]
    :emails/ban [:calendly.schedule/talk
                 :slack/remove-user
                 :loomio/remove-user
                 :trello/remove-user
                 :google-drive/unshare]
    :slack/remove-user []
    :loomio/remove-user []
    :trello/remove-user []
    :google-drive/unshare []}
   :slack
   {:slack/commands [:slack.commands/appear
                     :slack.commands/invite
                     :slack.commands/report
                     :slack.commands/talk]
    :slack.commands/appear [:appearin/new-video-conference]
    :appearin/new-video-conference []
    :slack.commands/invite [:typeform/invite]
    :slack.commands/report [:typeform/report]
    :slack.commands/talk [:calendly.schedule/talk]}})

(def alpha
  (let [flattened (apply merge-with (comp vec concat)
                         (vals flows))]
    (->> flattened
         vals
         (mapcat identity)
         (distinct)
         (<- (zipmap (repeat [])))
         (merge-with (comp vec concat) flattened))))

(defn node->key
  [s]
  (-> s
      str
      (str/replace #":" "")
      keyword))

(defn key->parent
  [k]
  (let [[k & ns] (some-> k
                         namespace
                         (str/split #"\.")
                         reverse)]
    (cond
      (seq ns) (keyword (str/join "." ns) k)
      k (keyword k)
      :else nil)))

(def cluster->parent
  (->> (for [k (keys alpha)
             :let [parent (key->parent k)]
             :when parent]
         [k parent])
       (into {})))

(defn top-cluster
  [k]
  (or (cluster->parent (key->parent k))
      (key->parent k)))

(defn nodes-in-flow
  [k]
  (let [flow (get flows k)
        flow-nodes (keys flow)
        after-flow (->> (vals flow)
                        (mapcat identity))
        before-flow (->> alpha
                         (filter (comp seq
                                       (partial set/intersection (set flow-nodes))
                                       set
                                       second))
                         (map first))]
    (distinct (concat flow-nodes after-flow before-flow))))

(defn flow-for-node
  [node]
  (first
   (for [[k flow] flows
         :when (get (set (keys flow)) node)]
     k)))

(->> flows
     (vals)
     (mapcat identity)
     (map first)
     distinct
     (map )
     (filter (comp seq next second)))

(def dir "graphs")
(fs/delete-dir dir)
(fs/mkdirs dir)

(viz/save-graph
 (keys alpha) alpha
 :filename (str dir "/alpha.png")
 :node->descriptor (partial hash-map :label))

(viz/save-graph
 (keys alpha) alpha
 :filename (str dir "/alpha-flows.png")
 :node->descriptor (partial hash-map :label)
 :cluster->descriptor (partial hash-map :label)
 :node->cluster (comp flow-for-node node->key))

(viz/save-graph
 (keys alpha) alpha
 :filename (str dir "/alpha-services.png")
 :node->descriptor (partial hash-map :label)
 :cluster->descriptor (partial hash-map :label)
 :node->cluster (comp top-cluster node->key))

(viz/save-graph
 (keys alpha) alpha
 :filename (str dir "/alpha-services-parents.png")
 :node->descriptor (partial hash-map :label)
 :cluster->descriptor (partial hash-map :label)
 :node->cluster (comp key->parent node->key)
 :cluster->parent cluster->parent)

(fs/mkdirs (str dir "/flows"))

(doseq [flow (keys flows)]
  (viz/save-graph
   (nodes-in-flow flow) alpha
   :filename (str dir "/flows/" (name flow) ".png")
   :node->descriptor (partial hash-map :label)))

(comment
  (viz/view-graph (keys (:public flows)) alpha
                  :node->descriptor (fn [n] {:label n}))

  )
