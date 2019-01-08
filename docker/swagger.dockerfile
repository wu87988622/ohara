FROM nginx

ARG NGINXPATH=/usr/share/nginx
COPY swagger/dist $NGINXPATH/html
COPY swagger/yamls $NGINXPATH/html/yamls
COPY swagger/generate_json.sh $NGINXPATH/html
COPY swagger/nginx.conf /etc/nginx/default.conf
COPY swagger/set_proxy.sh /etc/nginx/

RUN bash /usr/share/nginx/html/generate_json.sh

CMD ["bash","-c","/etc/nginx/set_proxy.sh && nginx -g \"daemon off;\""]