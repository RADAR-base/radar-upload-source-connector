#!/bin/sh

# Replace env vars in JavaScript files
echo "Replacing env vars in JS"
for file in /usr/share/nginx/html/js/app.*.js*;
do
  echo "Processing $file ...";

  # Use the existing JS file as template
  if [ ! -f $file.tmpl.js ]; then
    cp $file $file.tmpl.js
  fi

  sed -i 's|VUE_APP_API_BASE_URL|'${VUE_APP_API_BASE_URL}'|g' $file $file.tmpl.js
  sed -i 's|VUE_APP_AUTH_API|'${VUE_APP_AUTH_API}'|g' $file $file.tmpl.js
  sed -i 's|VUE_APP_AUTH_CALLBACK|'${VUE_APP_AUTH_CALLBACK}'|g' $file $file.tmpl.js
  sed -i 's|VUE_APP_CLIENT_ID|'${VUE_APP_CLIENT_ID}'|g' $file $file.tmpl.js
#  envsubst 'VUE_APP_BASE_URL,VUE_APP_AUTH_API,VUE_APP_AUTH_CALLBACK,VUE_APP_CLIENT_ID' < $file.tmpl.js > $file
  rm $file.tmpl.js
done

echo "Starting Nginx"
nginx -g 'daemon off;'
