# Django settings for OM project.
from os.path import dirname, join
import sys

DEBUG = True
TEMPLATE_DEBUG = DEBUG

ADMINS = (
     ('Peyman Kazemian', 'kazemian@stanford.edu'),
)

SRC_DIR = join(dirname(__file__), '../../../')

STATIC_DOC_ROOT = join(SRC_DIR, 'static/openflow/optin_manager')

MANAGERS = ADMINS

DATABASE_ENGINE = 'sqlite3'    # 'postgresql_psycopg2', 'postgresql', 'mysql', 'sqlite3' or 'oracle'.
DATABASE_NAME = join(SRC_DIR, '../db/openflow/optin_manager/om.db')  # Or path to database file if using sqlite3.
DATABASE_USER = ''             # Not used with sqlite3.
DATABASE_PASSWORD = ''         # Not used with sqlite3.
DATABASE_HOST = ''             # Set to empty string for localhost. Not used with sqlite3.
DATABASE_PORT = ''             # Set to empty string for default. Not used with sqlite3.

# Local time zone for this installation. Choices can be found here:
# http://en.wikipedia.org/wiki/List_of_tz_zones_by_name
# although not all choices may be available on all operating systems.
# If running in a Windows environment this must be set to the same as your
# system time zone.
TIME_ZONE = 'America/Los_Angeles'

# Language code for this installation. All choices can be found here:
# http://www.i18nguy.com/unicode/language-identifiers.html
LANGUAGE_CODE = 'en-us'

SITE_ID = 1

# If you set this to False, Django will make some optimizations so as not
# to load the internationalization machinery.
USE_I18N = True

# Absolute path to the directory that holds media.
# Example: "/home/media/media.lawrence.com/"
MEDIA_ROOT = join(STATIC_DOC_ROOT, "media")

# URL that handles the media served from MEDIA_ROOT. Make sure to use a
# trailing slash if there is a path component (optional in other cases).
# Examples: "http://media.lawrence.com", "http://example.com/media/"
MEDIA_URL = '/static/media'

# URL prefix for admin media -- CSS, JavaScript and images. Make sure to use a
# trailing slash.
# Examples: "http://foo.com/media/", "/media/".
ADMIN_MEDIA_PREFIX = '/admin/media/'

# Make this unique, and don't share it with anybody.
SECRET_KEY = '2f(jw$r445m^g3#1e)mysi2c#4ny83*4al=#adkj1o98ic+44i'

# List of callables that know how to import templates from various sources.
TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.load_template_source',
    'django.template.loaders.app_directories.load_template_source',
)

MIDDLEWARE_CLASSES = (
    'django.middleware.common.CommonMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
)

ROOT_URLCONF = 'openflow.optin_manager.urls'

TEMPLATE_DIRS = (
    # Put strings here, like "/home/html/django_templates" or "C:/www/django/templates".
    # Always use forward slashes, even on Windows.
    # Don't forget to use absolute paths, not relative paths.
    join(SRC_DIR, 'templates'),
    join(SRC_DIR, 'templates/openflow/optin_manager'),
)

INSTALLED_APPS = (
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.sites',
    'django.contrib.admin',
    'expedient.common.rpc4django',
    'expedient.common.xmlrpc_serverproxy',
    'expedient.common.defaultsite',
    'registration',
    'openflow.optin_manager.users',
    'openflow.optin_manager.flowspace',
    'openflow.optin_manager.xmlrpc_server',
    'optin_manager.omclient_tester',
###### For Testing #######################
    'openflow.optin_manager.dummyfv',
)

AUTH_PROFILE_MODULE = "users.UserProfile"

LOGIN_REDIRECT_URL = '/'

# E-Mail sending settings
#EMAIL_HOST = 'smtp.stanford.edu'
EMAIL_USE_TLS = True
EMAIL_HOST = 'smtp.gmail.com'
EMAIL_HOST_USER = 'geni.opt.in.manager@gmail.com'
EMAIL_HOST_PASSWORD = "stanfordom!"
EMAIL_PORT = 587
DEFAULT_FROM_EMAIL = 'no-reply@stanford.edu'
EMAIL_SUBJECT_PREFIX = '[GENI-Opt IN Manager]'

# Registration App settings
ACCOUNT_ACTIVATION_DAYS = 3

# XML-RPC settings
XMLRPC_TRUSTED_CA_PATH = '/etc/apache2/ssl.crt'
XMLRPC_TIMEOUT = 120
MY_CA = join(XMLRPC_TRUSTED_CA_PATH, 'ca.crt')

# default site
SITE_ID = 1
SITE_NAME = "Expedient Opt-In Manager"
SITE_DOMAIN = "beirut.stanford.edu"
