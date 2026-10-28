"""Reject image wrappers and nonstandard keys as ordinary HLS delivery evidence."""
import unittest
from unittest.mock import patch
import audit_channels


class HlsEvidenceTest(unittest.TestCase):
    def probe(self, playlist, segment, key=None):
        def get(url, headers=None, limit=1048576):
            body=playlist if url.endswith('index.m3u8') else key if url.endswith('key') else segment
            return body, {}, url
        with patch.object(audit_channels, 'get', side_effect=get):
            return audit_channels.media('https://example.com/index.m3u8', {})

    def test_nonstandard_key_cannot_pass_as_native_aes128(self):
        playlist=b'#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI="key"\n#EXTINF:4,\nsegment.ts\n'
        self.assertEqual('unsupported_aes128_key_length', self.probe(playlist,b'G'*188,b'x'*48))

    def test_image_wrapper_cannot_pass_as_a_media_segment(self):
        playlist=b'#EXTM3U\n#EXTINF:4,\nsegment.ts\n'
        self.assertEqual('wrapped_image_segment', self.probe(playlist,b'\x89PNG\r\n\x1a\n'+b'x'*100))

    def test_conventional_transport_still_passes_delivery_check(self):
        playlist=b'#EXTM3U\n#EXTINF:4,\nsegment.ts\n'
        self.assertEqual('hls_segment_ok', self.probe(playlist,b'G'+b'\x00'*187))


if __name__=='__main__':
    unittest.main()
