# Mutlaboc Notes

This project uses Firebase Authentication and Firestore.

If you encounter `PERMISSION_DENIED` errors when reading or writing notes,
deploy the provided `firestore.rules` file to your Firebase project:

```
firebase deploy --only firestore:rules
```

The rules restrict access to `users/{uid}/notes` so that each user can
only read and write their own notes.
