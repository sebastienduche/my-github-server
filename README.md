# my-github-server

This jar allows to implement an auto-update for an application:

- 2 jars need to be build to be able to have an auto-update. The first jar (ABCLauncher.jar) must contain only the
  classes related to the Update process. The second jar which contain the full application will be updated by the first
  jar.
- A simple class that extends MyLauncher and implements the install() function should be implemented.
- See other of my projects as example