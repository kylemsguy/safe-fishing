SafeFishing [![Slack][slackin-badge]][slackin]
============

_Geographical hazard tracking system, for the sea_

A mobile application that informs fishers of areas where chemical weapons and ammunition have been dumped in the world's oceans. Requires Android 4.0.3 and up.

Built for [Fishackathon 2015](http://fishackathon2015.challengepost.com/)

###About

Over the past century, people have been dumping hazardous materials into oceans all around the world. Some of the more dangerous ones include: ammunition, toxic waste, and nuclear weapons. These are not only hazardous to fishers; they also damage the ocean ecology and the quality of the fish.

Our app takes the safety of fishers, small and large alike, into our hands. By using geofencing and GPS technology, our app presents fishers with a user-friendly hazard tracking map. It combines databases selected by researchers with a beautiful interface to apply the newest technologies to something that humans have been doing for centuries: fishing.

###Technical

We are using an industry standard KML format to store the coordinates and information on hazard locations. This data is imported and parsed programmatically, making it easy to reach out to other databases and import the latest information regularly.

We've provided additional information and facts about the hazard locations to better aid in the users’ decision making and navigation. You can access them with a single touch.

We also looked at the needs of fishers and developed an application that is able to function without a good network connection, as long as there is cached data.

Currently, the application obtains hazard locations from the [Center of Non-Proliferation at the Monterey Institute of International Studies](http://cns.miis.edu/stories/090806_cw_dumping.htm); we are researching additional sources of information to add to the database.

###Screenshots

![Homepage](/app/src/main/assets/home.png "Homepage")  ![Notification](/app/src/main/assets/notification.png "Notification")  ![Settings](/app/src/main/assets/settings.png "Settings page")

[slackin]: https://fishyfishes.herokuapp.com/
[slackin-badge]: https://fishyfishes.herokuapp.com/badge.svg
