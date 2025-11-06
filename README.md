# PHP String Field

<p><strong>PHP StringField</strong> is a PhpStorm plugin that adds navigation and references support for string parameters of property names.<br>
Including nested fields accessed via dot notation (<code>'profile.avatar.url'</code>).</p>

## Features

- Navigation, reference, and find usages support.

## Usage

- Add the `@string-field` to a `@param` phpdoc tag, to enable navigation through string path.
- Use the `@string-field:call` to interpret the  **last field to be treated as a callable method**.

## Example

```php
class User {
     public Profile $profile;

    /**
     * @param string $path @string-field
     */
    public function field(string $path) {
        // ...
    }
}

$user = new User();
$user->field('profile.avatar.url');
```

Now you can navigate from `'profile.avatar.url'` directly to the corresponding property or method in the class.
