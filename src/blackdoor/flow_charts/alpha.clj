(ns blackdoor.flow-charts.alpha
  (:require [me.raynes.fs :as fs]
            [plumbing.core :refer :all]
            [rhizome.viz :as viz]
            [taoensso.timbre :as log]))

(def flows
  {:public
   {:com/myblackdoor [:blackdoor/member
                      :blackdoor/guest]
    :blackdoor/guest [:patreon/blackdoor
                      :typeform/subscribe]
    :blackdoor/member [:slack/signin
                       :loomio/signin]
    :loomio/signin [:loomio/subgroups]
    :slack/signin [:slack/commands]
    :facebook/myblackdoor [:com/myblackdoor]
    :instagram/myblackdoor [:com/myblackdoor]}
   :subscribe
   {:typeform/subscribe [:emails/subscribed
                         :mailchimp/blackdoor]
    :emails/subscribed [:patreon/blackdoor]}
   :invite
   {:typeform/invite [:emails/invite]
    :emails/invite [:typeform/apply]}
   :apply
   {:typeform/apply [:emails/applied
                     :loomio/applications]
    :emails/applied [:patreon/blackdoor]
    :loomio/applications [:loomio.application/approve
                          :loomio.application/reject]
    :loomio.application/approve [:emails/accepted]
    :emails/accepted [:slackpass/signup]
    :loomio.application/reject [:emails/rejected]}
   :signup
   {:slackpass/signup [:emails/signed-up
                       :docs/orientation
                       :loomio/new-user
                       :slack/new-user
                       :trello/new-user
                       :google-drive/new-user]
    :emails/signed-up [:docs/orientation
                       :calendly.schedule/onboarding
                       :patreon/blackdoor]
    :docs/orientation [:calendly.schedule/onboarding
                       :patreon/blackdoor
                       :loomio.subgroups/blackdoor
                       :slack.channels/blackdoor
                       :trello/blackdoor
                       :google-drive/blackdoor]}
   :report
   {:typeform/report [:loomio/moderation]}
   :slack
   {:slack/channels [:slack.channels/blackdoor
                     :slack.channels/story-time
                     :slack.channels/health-safety
                     :slack.channels/tech]
    :slack/commands [:slack.commands/appear
                     :slack.commands/invite
                     :slack.commands/report
                     :slack.commands/talk]
    :slack.commands/appear [:appear.in/new-video-conference]
    :slack.commands/invite [:typeform/invite]
    :slack.commands/report [:typeform/report]
    :slack.commands/talk [:calendly.schedule/talk]}
   :calendly
   {:calendly/schedule [:calendly.schedule/onboarding
                        :calendly.schedule/talk
                        :calendly.schedule/pleasetrythisathome
                        :calendly.schedule/sspectacularr]}
   :loomio
   {:loomio/subgroups [:loomio.subgroup/blackdoor
                       :loomio.subgroup/applications
                       :loomio.subgroup/moderation
                       :loomio.subgroup/tech]}
   :integrations
   {:loomio/subgroups [:slack.channels/blackdoor]
    :github/changes [:slack.channels/blackdoor]
    :google-drive/blackdoor [:slack.channels/blackdoor]}})

(def alpha
  (let [flattened (apply merge-with (comp vec concat)
                         (vals flows))]
    (->> flattened
         vals
         (mapcat identity)
         (distinct)
         (<- (zipmap (repeat [])))
         (merge-with (comp vec concat) flattened))))

(defn node->descriptor
  [n]
  {:label n})

(def dir "graphs")
(fs/mkdirs dir)

(defn viz
  ([] (viz true))
  ([save?]
   ((if save?
      viz/save-graph
      viz/view-graph)
    (keys alpha) alpha
    :node->descriptor node->descriptor
    :filename (str dir "/alpha.png"))))

(viz)

(comment
  (viz/view-graph (keys alpha) alpha
                  :node->descriptor (fn [n] {:label n}))



  )
