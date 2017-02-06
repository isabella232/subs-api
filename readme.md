# subs-frontend

REST server for the submissions project

## Endpoints

 * /api/submissions
   * view all the submissions
   * pageable
 * /api/submissions/{id}
   * view one submission
 * /api/browser
   * HAL browser, use it to play with other endpoints
 * /api/submissions/search
   * discover available search endpoints for submissions
 * /api/submissions/search/domain?domainName={name}
   * view all submissions for a domain
   * pageable
   
### Pageable

Pageable endpoints accept the following parameters (copied from Spring Data docs):

 * page - the page number to access (0 indexed, defaults to 0).
 * size - the page size requested (defaults to 20).
 * sort - a collection of sort directives in the format ($propertyname,)+[asc|desc]?.
 
There will be a page element in the response body:

	"page": {
	    "size": 20,
	    "totalElements": 1,
	    "totalPages": 1,
	    "number": 0
	  } 

There will also be a links element in the response body:

	"_links": {
	    "first": {
	      "href": "http://localhost:8080/api/submissions/search/domain?domainName=testDomain&page=0&size=1"
	    },
	    "self": {
	      "href": "http://localhost:8080/api/submissions/search/domain?domainName=testDomain&size=1"
	    },
	    "next": {
	      "href": "http://localhost:8080/api/submissions/search/domain?domainName=testDomain&page=1&size=1"
	    },
	    "last": {
	      "href": "http://localhost:8080/api/submissions/search/domain?domainName=testDomain&page=2&size=1"
	    }
	  }
