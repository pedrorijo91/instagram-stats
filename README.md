# instagram-stats [![Codacy Badge](https://api.codacy.com/project/badge/d34d6fa2e56944b3933ebfebbb1afc53)](https://www.codacy.com)

Play webapp that provides some stats about your Instagram relationships, using [Instagram API](https://instagram.com/developer/endpoints/).

## How to run
Simply use the command `sbt run` as any other play app

### Requirements
In the `conf/application.conf` file you need to insert the app `client ID` and `client secret`.
You can get those creating a new instagram app in [Instagram developer](https://instagram.com/developer/clients/register/).

## Problems due to Instagram API new restrictions
Due to the fact that Instagram API does not allows new apps to get access to relationships/likes scopes [unless the target users are business](https://help.instagram.com/contact/185819881608116), this app will not work.

### How to overcome
If you want to check stats for your own account just get an access token and replace it in `app/services/Instagram#getAccessToken`. 

You can get a token using [this url](https://apigee.com/console/instagram), providing authorization, making any endpoint call, and inspecting
the outgoing request, looking for the access token

## TODO

* make a prettier interface (I would spend time on that if the app could be deployed - hoping Instagram will reopen its API)
* take care of connection timeouts - currently throwing an error
* validate json responses
