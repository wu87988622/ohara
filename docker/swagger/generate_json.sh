#!/usr/bin/env bash

#  generate urls json
cd /usr/share/nginx/html
yamlsDir="./yamls/"
arr=( $(ls ./yamls/) )
urls="["
for i in "${!arr[@]}"
do
	 if [ "$i" == 0 ];then
	  urls="$urls{\"url\":\"$yamlsDir${arr[i]}\",\"name\":\"${arr[i]%.*}\"}"
	 else
	  urls="$urls,{\"url\":\"$yamlsDir${arr[i]}\",\"name\":\"${arr[i]%.*}\"}"
	 fi
done
urls="$urls]"
echo $urls > urls.json
