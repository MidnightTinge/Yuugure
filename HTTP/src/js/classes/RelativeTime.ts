const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const zp = (n: number) => n < 10 ? `0${n}` : `${n}`;

export default function RelativeTime(timestamp: number): string {
  const now = Date.now();

  let descriptor: string;
  let diff = now - timestamp;
  const future = diff < 0;
  diff = Math.abs(diff) / 1e3 >> 0;

  if (diff >= 604800) {
    // week
    const date = new Date(timestamp);
    return `${months[date.getMonth()]} ${date.getDate()} '${String(date.getFullYear()).substring(2)} ${zp(date.getHours())}:${zp(date.getMinutes())}`;
  } else if (diff >= 86400) {
    // day
    const days = diff / 86400;
    const hours = diff % 86400 / 3600;
    descriptor = `${days >> 0}d`;
    if ((hours >> 0) > 0) {
      descriptor += `${hours >> 0}h`;
    }
  } else if (diff >= 3600) {
    // hour
    const hours = diff / 3600;
    const minutes = diff % 3600 / 60;
    descriptor = `${hours >> 0}h`;
    if ((minutes >> 0) > 0) {
      descriptor += `${minutes >> 0}m`;
    }
  } else if (diff >= 60) {
    // minute
    const minutes = diff / 60;
    const seconds = diff % 60;
    descriptor = `${minutes >> 0}m`;
    if ((seconds >> 0) > 0) {
      descriptor += `${seconds >> 0}s`;
    }
  } else if (diff >= 10) {
    // seconds
    descriptor = `${diff >> 0}s`;
  } else {
    return 'just now';
  }

  return future ? `in ${descriptor}` : `${descriptor} ago`;
}
