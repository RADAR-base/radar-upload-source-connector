# alpine version close to the version mentioned in the readme
FROM --platform=$BUILDPLATFORM node:16 as builder

RUN mkdir /app
WORKDIR /app

COPY package*.json /app/
RUN npm install

COPY . /app/

RUN npm run build --prod

FROM nginxinc/nginx-unprivileged:1.27.4-alpine3.21-perl
USER root

# add init script
COPY ./docker/nginx.conf /etc/nginx/conf.d/default.conf

WORKDIR /usr/share/nginx/html

COPY --from=builder /app/dist/ .

COPY ./docker/entrypoint.sh /entrypoint.sh

# expose internal port:80 and run init.sh
EXPOSE 8080

USER 101
ENV NGINX_ENTRYPOINT_WORKER_PROCESSES_AUTOTUNE=1
ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
