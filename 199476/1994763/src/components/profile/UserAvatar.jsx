import { useEffect, useState } from 'react';
import { BadgeCheck, UserRound } from 'lucide-react';

export default function UserAvatar({ src, uid, name, className = 'avatar', verified = false }) {
  const [imageFailed, setImageFailed] = useState(false);
  const showImage = Boolean(src) && !imageFailed;

  useEffect(() => {
    setImageFailed(false);
  }, [src]);

  return (
    <div className={`${className}${showImage ? '' : ' default-avatar'}`}>
      {showImage ? (
        <img
          src={src}
          alt={`${name || (uid ? `UID ${uid}` : '用户')}的头像`}
          onError={() => setImageFailed(true)}
        />
      ) : (
        <UserRound aria-label="默认头像" />
      )}
      {verified && (
        <i>
          <BadgeCheck aria-hidden="true" />
        </i>
      )}
    </div>
  );
}
