FROM centos
ENV TOMCAT_SPECIFIC_VERSION_NAME=apache-tomcat-10.1.5
ENV TOMCAT_DOWNLOAD_LINK=https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.5/bin/$TOMCAT_SPECIFIC_VERSION_NAME.tar.gz

RUN cd /etc/yum.repos.d/
RUN sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*
RUN sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*

RUN yum -y install java

RUN mkdir /opt/tomcat/
WORKDIR /opt/tomcat

ADD $TOMCAT_DOWNLOAD_LINK /opt/tomcat
RUN tar xvfz apache*.tar.gz
RUN mv $TOMCAT_SPECIFIC_VERSION_NAME/* /opt/tomcat

EXPOSE 8080
CMD ["/opt/tomcat/bin/catalina.sh", "run"]