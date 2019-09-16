#!/bin/sh

# Replace env vars in JavaScript files
echo "Replacing env vars in JS"
for file in /usr/share/nginx/html/js/app.*.js*;
do
  echo "Processing $file ...";

  sed -i 's|VUE_APP_API_BASE_URL|'${VUE_APP_API_BASE_URL}'|g' $file
  sed -i 's|VUE_APP_AUTH_API|'${VUE_APP_AUTH_API}'|g' $file
  sed -i 's|VUE_APP_AUTH_CALLBACK|'${VUE_APP_AUTH_CALLBACK}'|g' $file
  sed -i 's|VUE_APP_CLIENT_ID|'${VUE_APP_CLIENT_ID}'|g' $file

done

echo "Starting Nginx"
nginx -g 'daemon off;'
