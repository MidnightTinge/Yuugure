*The app is currently in an Alpha state. It does what it says on the tin, but it's missing a lot of
secondary functionality. While everything may be present to perform its base job, the app is still
not ready for public consumption. See the license section below.*

# Yuugure

Full-stack tag-based image archive. Stack:

* Java (Undertow)
* Prometheus (optional, for monitoring)
* PostgreSQL
* Redis
* Elasticsearch
* React (TypeScript)
* TailwindCSS (PostCSS)

# Software Needed

* PostgreSQL 13
* Redis 6
* Elasticsearch 7.13.2
* Java 15
* NodeJS 15.8.0+ (front-end builds)
  * The maven build will automatically download a NodeJS distribution for automated builds, you only
    need NodeJS yourself if you want to build the front-end manually.
* FFMPEG
  * Required by the MediaProcessor. Tested with v4.4, but may work for prior versions.

# License

This project is currently unlicensed, therefore the project owner maintains all retainable
copyrights.
