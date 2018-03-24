# MediaTransmitter

![Component Diagram](/ComponentDiagram.jpg)

## Description

Is a tool for sending photos, videos and multimedia data in general over a websocket connection between two or multiple android devices. Sent files can be viewed via an angular frontend, such as microservice health and statistics.

## About resilience

### Case 1: Authentication service is down

* App still works. However, you cannot login to your normal account but you still can send media data via a guest profile and you receive temporary guest login data.
* Front end still works. You can view your photos and videos using your temporary login data from the app

### Case 2: Frontend instance is down

* App still works. User is logged in and every sent media data is saved temporary in local database and will be synchronised as soon as the instance is going back online. After going online again, the user can login to the frontend with is account and can view every sent photo, video, etc.
* Authenication service still works. User can login on the app.

### Case 3: Websocket service is down

* Front end still works. User can login with their accounts and are able to view all their sent data.
* Authenication service still works. Users are authenticated.
