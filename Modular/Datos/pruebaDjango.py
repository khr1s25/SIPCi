place()
from django.shortcuts import render

def place (request):
    query = request.GET.get('name')
    messege = "hello {} i am learning django".format(query)
    template = "place.html"
    context = {
        'message': message,
        }
        return render ( request, template, context)