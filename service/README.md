# Installing the Notes App Service

1. Create an S3 scratch bucket (optional: only if you don't have something)
2. Copy the YAML file to the scratch bucket
3. Create the CloudFormation Stack
4. Generate the awsconfiguration.json file

## Create an S3 scratch bucket

Using the AWS CLI:

```
$ aws s3 mb s3://bucket-name
```

The bucket name must be globally unique.  

## Copy the YAML file to the scratch bucket

Using the AWS CLI:

```
$ aws s3 sync . s3://bucket-name
```

This will copy the current directory to the S3 bucket.  You can also place the `NotesApp.yaml` into the S3 bucket in a sub-directory.

## Create the CloudFormation Stack

```
$ aws cloudformation validate-template \
  --template-url https://s3-us-west-2.amazonaws.com/bucket-name/NotesApp.yaml
```

Note that the template-url is a HTTPS endpoint based on both the region and the bucket name.  You must have the right URL.  It's available in the S3 console if you are unsure of it.

```
$ aws cloudformation create-stack \
  --stack-name notes-app \
  --template-url https://s3-us-west-2.amazonaws.com/bucket-name/NotesApp.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters ParameterKey=APIName,ParameterValue=NotesApp
```

Again, use the right template URL

## Generate the awsconfiguration.json file

Use the resources to generate a suitable awsconfiguration.json - you will need
to use the `aws cloudformation describe-stack-resources` to get the list of resources, then individual aws commands.

... Or use my script.

