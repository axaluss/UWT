logLevel := Level.Warn

resolvers += Resolver.sonatypeRepo("public")
//https://oss.sonatype.org/content/groups/public

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")