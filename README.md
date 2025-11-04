# PHP String Field

<p><strong>PHP StringField</strong> is a PhpStorm plugin that adds navigation and reference support for string parameters.<br>
Including nested fields accessed via dot notation (<code>'user.profile.email'</code>).</p>

## Features

- Navigation, reference, and find usages support.

## Usage

- Add the `@string-field` to a `@param` phpdoc tag, to enable navigation through string paths.
- Use the `@string-field:call` to interpret the  **last field to be treated as a callable method** if it exists in the
  referenced path.

## Example

```php
class User {
     public Email $email;
     public Profile $profile;

    /**
     * @param string $path @string-field:call
     */
    public function field(string $path) {
        // ...
    }
}

$user = new User();
$user->field('profile.avatar.getUrl');
```

Now you can navigate from `'profile.avatar.getUrl'` directly to the corresponding property or method in the class.
