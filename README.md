WebsiteSearcher is used to determine if a given searchTerm exists across a bunch of websites

input.txt - This file consists of the url information copied from the remote s3 file "https://s3.amazonaws.com/fieldlens-public/urls.txt"
results.txt - This file tells us whether a search term exists on the page. It is of the format "Url","SearchTerm","IsPresent"

Execution:
java -jar WebsiteSearcher.jar



