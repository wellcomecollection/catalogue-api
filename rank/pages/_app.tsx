import { AppProps } from 'next/app'
import Head from 'next/head'
import '../styles/app.css'

export default function WecoApp({ Component, pageProps }: AppProps) {
  return (
    <>
      <Head>
        <link
          rel="apple-touch-icon"
          sizes="180x180"
          href="/apple-touch-icon.png"
        />
        <link
          rel="icon"
          type="image/png"
          sizes="32x32"
          href="/favicon-32x32.png"
        />
        <link
          rel="icon"
          type="image/png"
          sizes="16x16"
          href="/favicon-16x16.png"
        />
      </Head>
      <div className="px-4 py-2 lg:max-w-3xl max-w-2xl">
        <div>
          <Component {...pageProps} />
        </div>
      </div>
    </>
  )
}
