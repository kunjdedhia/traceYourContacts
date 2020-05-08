/** 
  * @desc Assignment 5 COVID-19 App
  * @name Kunj Dedhia kdedhia@umass.edu
*/

The following features have been implemented - 
1. The application tracks the number of unique bluetooth devices it discovers and the corresponding the location coordinates. This information is stored locally in txt files sorted by dates. Since this information is stored locally, it protects the user's privacy and can be used if necessary with the user's consent.

2. It assigns a social distancing score. Every user starts with a 100 at the start of the day and as the day progresses, if the user comes in close contact of other bluetooth devices or visits crowded places, it looses points. The goal is to have the highest score at the end of the day.

3. The launching screen also plots a heat map of the places the user has visited. Such a graphic visualization is really helpful to the user to track its daily movements.

4. The app broadcasts notifications throughout day for various events. In my opinion, reminders are a great way to keep the user informed of his performance and encourages them to do better
	a. A visit to a crowded place
	b. Close contact with another bluetooth device
	c. A sudden drop in score due to any plausible reason


Git Repo - https://github.com/kunjdedhia/traceYourContacts
Video - 