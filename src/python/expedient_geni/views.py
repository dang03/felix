'''
@author: jnaous
'''
import os
import logging
from django.core.urlresolvers import reverse
from django.conf import settings
from expedient.common.utils.views import generic_crud
from forms import geni_aggregate_form_factory
from expedient.common.permissions.shortcuts import give_permission_to,\
    must_have_permission
from django.shortcuts import get_object_or_404
from django.contrib.auth.models import User
from django.views.generic import simple
from expedient_geni.utils import get_user_cert_fname, get_user_urn,\
    get_user_key_fname, create_x509_cert
from django.http import HttpResponseRedirect
from expedient.common.messaging.models import DatedMessage

logger = logging.getLogger("GENIViews")
TEMPLATE_PATH = "expedient_geni"

def aggregate_create(request, agg_model):
    '''
    Create a GENI Aggregate.
    
    @param request: The request.
    @param model: The child subclass for the aggregate.
    '''
    
    def pre_save(instance, created):
        instance.owner = request.user
    
    def post_save(instance, created):
        instance.update_resources()
        give_permission_to(
            "can_use_aggregate",
            instance,
            request.user,
            can_delegate=True
        )
        give_permission_to(
            "can_edit_aggregate",
            instance,
            request.user,
            can_delegate=True
        )
    
    def success_msg(instance):
        return "Successfully created aggregate %s." % instance.name
    
    return generic_crud(
        request, obj_id=None, model=agg_model,
        template=TEMPLATE_PATH+"/aggregate_crud.html",
        redirect=lambda instance:reverse("home"),
        form_class=geni_aggregate_form_factory(agg_model),
        pre_save=pre_save,
        post_save=post_save,
        extra_context={
            "create": True,
            "name": agg_model._meta.verbose_name,
        },
        success_msg=success_msg)
    
def aggregate_edit(request, agg_id, agg_model):
    """
    Update a GENI Aggregate.
    
    @param request: The request object
    @param agg_id: the aggregate id
    @param agg_model: the GENI Aggregate subclass.
    """
    
    def success_msg(instance):
        return "Successfully updated aggregate %s." % instance.name
    
    return generic_crud(
        request, obj_id=agg_id, model=agg_model,
        template=TEMPLATE_PATH+"/aggregate_crud.html",
        template_object_name="aggregate",
        redirect=lambda instance:reverse("home"),
        form_class=geni_aggregate_form_factory(agg_model),
        success_msg=success_msg)

def user_cert_manage(request, user_id):
    """Allow the user to download/regenerate/upload a GCF certificate.
    
    @param request: the request object
    @param user_id: the id of the user whose certificate we are managing.
    """
    
    user = get_object_or_404(User, pk=user_id)
    
    must_have_permission(request.user, user, "can_change_certs")
    
    cert_fname = get_user_cert_fname(user)
    if not os.access(cert_fname, os.F_OK):
        cert_fname = None
    
    return simple.direct_to_template(
        request,
        template=TEMPLATE_PATH+"/user_cert_manage.html",
        extra_context={
            "user": user,
            "cert_fname": cert_fname,
        },
    )

def user_cert_generate(request, user_id):
    """Create a new user certificate after confirmation.
    
    @param request: the request object
    @param user_id: the id of the user whose certificate we are generating.
    """
    
    user = get_object_or_404(User, pk=user_id)
    
    must_have_permission(request.user, user, "can_change_certs")
    
    cert_fname = get_user_cert_fname(user)
    key_fname = get_user_key_fname(user)
    urn = get_user_urn(user)

    if request.method == "POST":
        create_x509_cert(urn, cert_fname, key_fname)
        DatedMessage.objects.post_message_to_user(
            "GCF Certificate for user %s successfully created.",
            user=request.user, msg_type=DatedMessage.TYPE_SUCCESS)
        return HttpResponseRedirect(reverse(user_cert_manage, args=[user.id]))
    
    return simple.direct_to_template(
        request,
        template=TEMPLATE_PATH+"/user_cert_create.html",
        extra_context={
            "user": user,
        },
    )
        